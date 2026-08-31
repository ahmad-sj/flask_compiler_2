from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/item/<int:i>')
def m():
    x = 1
    return render_template('i.jinja')

@app.route('/item/<int:i>')
def n():
    y = 2
    return render_template('i.jinja')
