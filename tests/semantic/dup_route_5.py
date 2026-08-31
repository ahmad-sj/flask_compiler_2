from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/')
def t():
    x = 1
    return render_template('i.jinja')

@app.route('/b')
def u():
    y = 2
    return render_template('i.jinja')

@app.route('/')
def v():
    z = 3
    return render_template('i.jinja')
