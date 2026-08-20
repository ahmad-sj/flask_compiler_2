
from flask import Flask, render_template

app = Flask(__name__)

items = []

stock = 0

label = "widget"

@app.route('/')
def index():
    return render_template('branches.jinja', items=items, stock=stock, label=label)
