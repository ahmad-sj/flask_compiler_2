from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/c')
def c():
    if False:
        p = 1
    else:
        q = 2
    v = q
    return render_template('i.jinja')
