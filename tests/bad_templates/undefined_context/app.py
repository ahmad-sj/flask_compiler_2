from flask import Flask, render_template

app = Flask(__name__)

rows = [{"id": 1, "name": "A"}]

@app.route('/')
def index():
    return render_template('index.jinja', rows=rows)
