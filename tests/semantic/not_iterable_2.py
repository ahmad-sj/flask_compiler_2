from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/a')
def a():
    n = 3.5
    for v in n:
        w = v
    return render_template('i.jinja')
