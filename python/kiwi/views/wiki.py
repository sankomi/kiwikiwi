from datetime import datetime, timedelta
from random import randint
import math
import re
from urllib.parse import unquote

from flask import Blueprint, render_template, request, redirect, url_for, abort
from diff_match_patch import diff_match_patch
from sqlalchemy.exc import OperationalError
from markdown import markdown
import bleach
from bs4 import BeautifulSoup

from kiwi import db
from kiwi.models.wiki import Page, History
from kiwi.error import TitleDuplicateException, PageLockException


TITLE_REGEX = "[()\[\]\\n\\r*_`/\\\\]"
LINK_REGEX = "\[\[([^()\[\]\n\r*_`/\\\\]*)\]\]"
LINK_REPLACE = "[\\1](/wiki/\\1)"


bp = Blueprint("wiki", __name__)


@bp.route("/search")
def search():
    string = request.args.get("s")
    if not string:
        return render_template("search.html", search="", pages=None)
    string = string.strip()

    current = request.args.get("p")
    try:
        current = int(current)
        if current <= 0:
            current = 1
    except:
        current = 1

    pages = Page.query.filter(Page.text.ilike(f"%{string}%") | Page.title.ilike(f"%{string}%")).all()
    last = math.ceil(len(pages) / 10)
    show = pages[(current - 1) * 10:current * 10]
    return render_template("search.html", search=string, pages=show, current=current, last=last)


@bp.route("/wiki/<title>")
def view(title):
    page = Page.query.filter_by(title=title).first()

    if page is None:
        if re.search(TITLE_REGEX, title):
            abort(404)
        return render_template("not-exist.html", title=title)
    else:
        return render_template("view.html", page=page)


@bp.route("/wiki")
def random():
    count = Page.query.count()

    if count > 0:
        index = randint(0, count - 1)
        page = Page.query.offset(index).first()
        return redirect(url_for("wiki.view", title=page.title))
    else:
        return redirect(url_for("wiki.view", title="kiwikiwi"))


@bp.route("/back/<title>/<int:event>")
def back(title, event):
    back = make(title, event)

    if back is None:
        if re.search(TITLE_REGEX, title):
            abort(404)
        return redirect(url_for("wiki.history", title=title))
    else:
        return render_template("back.html", title=title, page=back, event=event)


@bp.route("/edit/<title>", methods=("GET", "POST"))
def edit(title, summary=None):
    page = Page.query.filter_by(title=title).first()

    if request.method == "GET":
        if page is None:
            if re.search(TITLE_REGEX, title):
                abort(404)
            page = Page(title=title, content="")

        return render_template("edit.html", page=page, summary=summary)
    elif request.method == "POST":
        new_title = request.form["title"]
        content = request.form["content"]
        summary = request.form["summary"]

        if re.search(TITLE_REGEX, new_title):
            new_title = replace(new_title, TITLE_REGEX, "")
            page = Page(title=new_title, content=content)
            return render_template("edit.html", page=page, summary=summary)

        try:
            updated = update(title, new_title, content, summary)
        except (TitleDuplicateException, PageLockException) as e:
            page.title = title
            page.content = content
            return render_template("edit.html", page=page, summary=summary)

        return redirect(url_for("wiki.view", title=updated.title))


@bp.route("/history/<title>")
def history(title):
    page = Page.query.filter_by(title=title).first()

    if page is None:
        if re.search(TITLE_REGEX, title):
            abort(404)
        return redirect(url_for("wiki.view", title=title))
    else:
        last = math.ceil(len(page.historys) / 10)
        historys = page.historys[:10]
        return render_template("history.html", page=page, current=1, last=last, historys=historys)


@bp.route("/history/<title>/<int:current>")
def history_page(title, current):
    page = Page.query.filter_by(title=title).first()

    if page is None:
        if re.search(TITLE_REGEX, title):
            abort(404)
        return redirect(url_for("wiki.view", title=title))
    else:
        last = math.ceil(len(page.historys) / 10)
        historys = page.historys[(current - 1) * 10:current * 10]
        return render_template("history.html", page=page, current=current, last=last, historys=historys)


@bp.route("/diff/<title>/<int:event>")
def diff(title, event):
    page = Page.query.filter_by(title=title).first()
    if page is None:
        if re.search(TITLE_REGEX, title):
            abort(404)
        return redirect(url_for("wiki.view", title=title))

    history = History.query.filter_by(event=event, page_id=page.id).first()

    if history is None:
        return redirect(url_for("wiki.history", title=title))

    title_diff = history.title
    title_diff = replace(title_diff, "\n\+([^\n]*)", "\n##ins##+\\1##/ins##")
    title_diff = replace(title_diff, "\n\-([^\n]*)", "\n##del##-\\1##/del##")
    title_diff = replace(title_diff, "@@\s\-\d+,{0,1}\d*\s\+\d+,{0,1}\d*\s@@\n{0,1}", "")
    title_diff = bleach.clean(unquote(title_diff))
    title_diff = replace(title_diff, "##(ins|/ins|del|/del)##", "<\\1>")
    content_diff = history.content
    content_diff = replace(content_diff, "\n\+([^\n]*)", "\n##ins##+\\1##/ins##")
    content_diff = replace(content_diff, "\n\-([^\n]*)", "\n##del##-\\1##/del##")
    content_diff = replace(content_diff, "@@\s\-\d+,{0,1}\d*\s\+\d+,{0,1}\d*\s@@\n{0,1}", "")
    content_diff = replace(content_diff, "(%0D)*%0A", "%0A ")
    content_diff = bleach.clean(unquote(content_diff))
    content_diff = replace(content_diff, "##(ins|/ins|del|/del)##", "<\\1>")

    return render_template("diff.html", title=title, page=page, history=history, diffs=(title_diff, content_diff))

def replace(string, find, replace):
    return re.sub(pattern=find, repl=replace, string=string)


@bp.route("/rehash/<title>/<int:event>")
def rehash(title, event):
    back = make(title, event)

    if back is None:
        if re.search(TITLE_REGEX, title):
            abort(404)
        return redirect(url_for("wiki.view", title=title))

    try:
        updated = update(title, back.title, back.content, "rehash(" + str(event) + ")")
    except (TitleDuplicateException, PageLockException) as e:
        return redirect(url_for("wiki.back", title=title, event=event))

    return redirect(url_for("wiki.view", title=updated.title))


def make(title, event):
    page = Page.query.filter_by(title=title).first()
    if page is None:
        return None

    history = History.query.filter_by(event=event, page_id=page.id).first()

    if page is None or history is None:
        return None

    back_title = ""
    back_content = ""

    for history in reversed(page.historys):
        if history.event > event:
            break
        back_title = apply_patch(back_title, history.title)
        back_content = apply_patch(back_content, history.content)

    back_content_linked = replace(back_content, LINK_REGEX, LINK_REPLACE)
    back_html = markdown(bleach.clean(back_content_linked))

    return Page(title=back_title, content=back_content, html=back_html)


def update(title, new_title, content, summary):
    page = Page.query.filter_by(title=title).first()

    if title != new_title:
        new_page = Page.query.filter_by(title=new_title).first()

        if new_page is not None:
            raise TitleDuplicateException("page with new title already exists")

    if page is None:
        content_linked = replace(content, LINK_REGEX, LINK_REPLACE)
        html = markdown(bleach.clean(content_linked))
        text = BeautifulSoup(html, features="html.parser").get_text()
        page = Page(title=new_title, content=content, html=html, text=text)
        db.session.add(page)

        title_patch = get_patch("", new_title)
        content_patch = get_patch("", content)

        history = History(page=page, summary=summary or "create", title=title_patch, content=content_patch)
        db.session.add(history)

        db.session.commit()

        return page

    if page.lock is not None and page.lock <= datetime.now():
        page.lock_id = None
        page.lock = None
        db.session.commit()

    lock_id = randint(0, 2147483647)
    lock = datetime.now() + timedelta(seconds=60)

    try:
        Page.query.filter_by(id=page.id, lock_id=None).update(dict(lock=lock, lock_id=lock_id))
        db.session.commit()
    except OperationalError as e:
        db.session.rollback()

    locked = Page.query.filter_by(title=title, lock=lock, lock_id=lock_id).first()

    if locked is None:
        raise PageLockException("page is locked")

    title_patch = get_patch(locked.title, new_title)
    content_patch = get_patch(locked.content, content)
    event = locked.historys[0].event + 1
    history = History(page=locked, summary=summary or "edit", title=title_patch, content=content_patch, event=event, write=datetime.now())
    db.session.add(history)

    content_linked = replace(content, LINK_REGEX, LINK_REPLACE)
    html = markdown(bleach.clean(content_linked))
    text = BeautifulSoup(html, features="html.parser").get_text()
    locked.title = new_title
    locked.content = content
    locked.html = html
    locked.text = text
    locked.lock = None
    locked.lock_id = None
    locked.refresh = datetime.now()
    db.session.commit()

    return locked


def get_patch(text1, text2):
    dmp = diff_match_patch()
    diff = dmp.diff_main(text1, text2, False)
    dmp.diff_cleanupSemantic(diff)
    patch = dmp.patch_make(diff)
    return dmp.patch_toText(patch)


def apply_patch(text, patch_text):
    dmp = diff_match_patch()
    patch = dmp.patch_fromText(patch_text)
    new_text, _ = dmp.patch_apply(patch, text)
    return new_text
