from datetime import datetime, timedelta
from random import randint

from flask import Blueprint, render_template, request, redirect, url_for
from diff_match_patch import diff_match_patch
from sqlalchemy.exc import OperationalError

from kiwi import db
from kiwi.models.wiki import Page, History

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


@bp.route("/wiki/<title>/<int:event>")
def back(title, event):
    page = Page.query.filter_by(title=title).first()
    history = History.query.filter_by(event=event, page_id=page.id).first()

    if history is None:
        return redirect(url_for("wiki.history", title=title))

    back_title = ""
    back_content = ""

    for history in reversed(page.historys):
        if history.event > event:
            break
        back_title = apply_patch(back_title, history.title)
        back_content = apply_patch(back_content, history.content)

    back = Page(title=back_title, content=back_content)

    return render_template("back.html", page=back, event=event)


@bp.route("/edit/<title>", methods=("GET", "POST"))
def edit(title):
    page = Page.query.filter_by(title=title).first()

    if request.method == "GET":
        if page is None:
            page = Page(title=title, content="")

        return render_template("edit.html", page=page)
    elif request.method == "POST":
        title = request.form["title"]
        content = request.form["content"]

        if page is None:
            page = Page(title=title, content=content)
            db.session.add(page)

            title_patch = get_patch("", title)
            content_patch = get_patch("", content)

            history = History(page=page, summary="update", title=title_patch, content=content_patch)
            db.session.add(history)

            db.session.commit()

            return redirect(url_for("wiki.view", title=page.title))

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
            page.title = title
            page.content = content
            return render_template("edit.html", page=page)

        title_patch = get_patch(locked.title, title)
        content_patch = get_patch(locked.content, content)
        event = locked.historys[0].event + 1
        history = History(page=locked, summary="update", title=title_patch, content=content_patch, event=event)
        db.session.add(history)

        locked.title = title
        locked.content = content
        locked.lock = None
        locked.lock_id = None
        locked.refresh = datetime.now()
        db.session.commit()

        return redirect(url_for("wiki.view", title=locked.title))


@bp.route("/history/<title>")
def history(title):
    page = Page.query.filter_by(title=title).first()

    if page is None:
        return redirect(url_for("wiki.view", title=title))
    else:
        return render_template("history.html", page=page)


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
