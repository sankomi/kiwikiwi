from flask import Flask
from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

def create_app(test_config=None):
    app = Flask(__name__, static_url_path="")
    app.url_map.strict_slashes = False

    app.config["SQLALCHEMY_DATABASE_URI"] = "sqlite:///kiwi.db"
    app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
    db.init_app(app)

    from .models import wiki

    from .views import main, wiki
    app.register_blueprint(main.bp)
    app.register_blueprint(wiki.bp)

    return app
