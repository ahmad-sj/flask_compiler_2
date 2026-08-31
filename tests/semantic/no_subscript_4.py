from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/c')
def c():
    k = 99
    v = k[2]
    return render_template('i.jinja')
