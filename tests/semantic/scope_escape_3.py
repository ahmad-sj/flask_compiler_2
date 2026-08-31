from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/b')
def b():
    n = 0
    while n < 2:
        seen = n
        n = n + 1
    z = seen
    return render_template('i.jinja')
