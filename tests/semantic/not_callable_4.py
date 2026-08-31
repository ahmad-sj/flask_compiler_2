from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/c')
def c():
    lst = [1]
    v = lst()
    return render_template('i.jinja')
