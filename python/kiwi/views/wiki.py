from flask import Blueprint, render_template

from kiwi.models.page import Page

bp = Blueprint("wiki", __name__)

@bp.route("/wiki/<title>")
def view(title):
    page = Page.query.filter_by(title=title).first()
    return render_template("view.html", page=page)
