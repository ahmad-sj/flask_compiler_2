from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/c')
def c():
    total = 42
    for t in total:
        u = t
    return render_template('i.jinja')
