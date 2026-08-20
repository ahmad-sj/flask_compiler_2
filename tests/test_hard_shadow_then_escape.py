# A builtin is shadowed, and a loop-local name escapes its block.
from flask import Flask, render_template

app = Flask(__name__)

@app.route('/')
def index():
    list = [1, 2, 3]
    for entry in list:
        doubled = entry * 2
    return render_template('i.jinja', v=doubled)
