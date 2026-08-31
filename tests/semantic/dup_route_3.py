from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/x')
def p():
    x = 1
    return render_template('i.jinja')

@app.route('/x')
def q():
    y = 2
    return render_template('i.jinja')

@app.route('/z')
def s():
    z = 3
    return render_template('i.jinja')
