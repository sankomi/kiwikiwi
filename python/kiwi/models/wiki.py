from datetime import datetime

from kiwi import db

class Page(db.Model):
    __tablename__ = "pages"

    id = db.Column(db.Integer, primary_key=True)
    title = db.Column(db.String(50), unique=True, nullable=False)
    content = db.Column(db.Text)
    lock = db.Column(db.DateTime)
    lock_id = db.Column(db.Integer)
    refresh = db.Column(db.DateTime, default=datetime.now())

    def __repr__(self):
        return f"<Page(id={self.id}, title={self.title}>"

class History(db.Model):
    __tablename__ = "historys"

    id = db.Column(db.Integer, primary_key=True)
    page_id = db.Column(db.Integer, db.ForeignKey("pages.id", ondelete="CASCADE"), nullable=False)
    page = db.relationship("Page", backref=db.backref("historys", cascade="all, delete-orphan"))
    summary = db.Column(db.String(100))
    title = db.Column(db.Text)
    content = db.Column(db.Text)
    write = db.Column(db.DateTime, default=datetime.now())

    def __repr__(self):
        return f"<History(id={self.id}, page_id={self.page_id}>"
