from flask import Flask, render_template

def create_app(test_config=None):
    app = Flask(__name__)

    @app.route("/")
    def kiwi():
        return render_template("index.html")

    return app
