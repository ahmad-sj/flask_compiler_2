from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/')
def index():
    for i in [1, 2]:
        inner = i
    x = inner
    return render_template('i.jinja')
