from kiwi import db
from datetime import datetime

class Test(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    text = db.Column(db.String(100), nullable=False)
    date = db.Column(db.DateTime(), nullable=False, default=datetime.now())

    def __repr__(self):
        return f"<Test {self.id} {self.text}>"

class Hello(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    hello = db.Column(db.String(100), nullable=False)
    test_id = db.Column(db.Integer, db.ForeignKey("test.id", ondelete="CASCADE"))
    test = db.relationship("Test", backref=db.backref("hellos", cascade="all, delete-orphan"))

    def __repr__(self):
        return f"<Hello {self.id} {self.hello} {self.test_id}>"
