from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/c')
def c():
    if 4.5 >= 'd':
        w = 1
    return render_template('i.jinja')
