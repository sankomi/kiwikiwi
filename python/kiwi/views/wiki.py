from datetime import datetime

from flask import Blueprint, render_template, request, redirect, url_for

from kiwi import db
from kiwi.models.page import Page

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
            return render_template("create.html", title=title)
        else:
            return render_template("edit.html", page=page)
    elif request.method == "POST":
        title = request.form["title"]
        content = request.form["content"]

        if page == None:
            page = Page(title=title, content=content)
        else:
            page.title = title
            page.content = content
            page.refreshed = datetime.now()

        db.session.add(page)
        db.session.commit()

        return redirect(url_for("wiki.view", title=page.title))
