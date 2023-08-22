from flask import Flask

def create_app(test_config=None):
    app = Flask(__name__)

    from .views import main
    app.register_blueprint(main.bp)

    return app
