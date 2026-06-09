from flask import Flask

app = Flask(__name__)

@app.route('/')
def index():
    app.run()
    return "ok"
