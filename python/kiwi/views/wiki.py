from datetime import datetime, timedelta
from random import randint

from flask import Blueprint, render_template, request, redirect, url_for

from kiwi import db
from kiwi.models.wiki import Page

bp = Blueprint("wiki", __name__)


@bp.route("/wiki/<title>")
def view(title):
    page = Page.query.filter_by(title=title).first()

    if page is None:
        return render_template("not-exist.html", title=title)
    else:
        return render_template("view.html", page=page)


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
            db.session.commit()

            return redirect(url_for("wiki.view", title=page.title))

        if page.lock is not None and page.lock <= datetime.now():
            page.lock_id = None
            page.lock = None
            db.session.commit()

        lock_id = randint(0, 2147483647)
        lock = datetime.now() + timedelta(seconds=60)
        Page.query.filter_by(id=page.id, lock_id=None).update(dict(lock=lock, lock_id=lock_id))
        db.session.commit()

        locked = Page.query.filter_by(title=title, lock=lock, lock_id=lock_id).first()

        if locked is None:
            page.title = title
            page.content = content
            return render_template("edit.html", page=page)

        locked.title = title
        locked.content = content
        locked.lock = None
        locked.lock_id = None
        locked.refresh = datetime.now()
        db.session.commit()

        return redirect(url_for("wiki.view", title=locked.title))
