from datetime import datetime, timedelta
from random import randint
import math

from flask import Blueprint, render_template, request, redirect, url_for
from diff_match_patch import diff_match_patch
from sqlalchemy.exc import OperationalError

from kiwi import db
from kiwi.models.wiki import Page, History
from kiwi.error import TitleDuplicateException, PageLockException

bp = Blueprint("wiki", __name__)


@bp.route("/wiki/<title>")
def view(title):
    page = Page.query.filter_by(title=title).first()

    if page is None:
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
        return redirect(url_for("wiki.history", title=title))

    return render_template("back.html", title=title, page=back, event=event)


@bp.route("/edit/<title>", methods=("GET", "POST"))
def edit(title, summary=None):
    page = Page.query.filter_by(title=title).first()

    if request.method == "GET":
        if page is None:
            page = Page(title=title, content="")

        return render_template("edit.html", page=page, summary=summary)
    elif request.method == "POST":
        new_title = request.form["title"]
        content = request.form["content"]
        summary = request.form["summary"]

        if "/" in new_title:
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
        return redirect(url_for("wiki.view", title=title))
    else:
        num_pages = math.ceil(len(page.historys) / 10)
        historys = page.historys[0:10]
        return render_template("history.html", page=page, pages=(1, num_pages), historys=historys)


@bp.route("/history/<title>/<int:page>")
def history_page(title, page):
    the_page = Page.query.filter_by(title=title).first()

    if the_page is None:
        return redirect(url_for("wiki.view", title=title))
    else:
        num_pages = math.ceil(len(the_page.historys) / 10)
        historys = the_page.historys[(page - 1) * 10:page * 10]
        return render_template("history.html", page=the_page, pages=(page, num_pages), historys=historys)


@bp.route("/diff/<title>/<int:event>")
def diff(title, event):
    page = Page.query.filter_by(title=title).first()
    history = History.query.filter_by(event=event, page_id=page.id).first()

    if page is None or history is None:
        return redirect(url_for("wiki.history", title=title))

    return render_template("diff.html", title=title, page=page, history=history)


@bp.route("/rehash/<title>/<int:event>")
def rehash(title, event):
    back = make(title, event)

    if back is None:
        return redirect(url_for("wiki.view", title=title))

    try:
        updated = update(title, back.title, back.content, "rehash(" + str(event) + ")")
    except (TitleDuplicateException, PageLockException) as e:
        return redirect(url_for("wiki.back", title=title, event=event))

    return redirect(url_for("wiki.view", title=updated.title))


def make(title, event):
    page = Page.query.filter_by(title=title).first()
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

    return Page(title=back_title, content=back_content)


def update(title, new_title, content, summary):
    page = Page.query.filter_by(title=title).first()

    if title != new_title:
        new_page = Page.query.filter_by(title=new_title).first()

        if new_page is not None:
            raise TitleDuplicateException("page with new title already exists")

    if page is None:
        page = Page(title=new_title, content=content)
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

    locked.title = new_title
    locked.content = content
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
