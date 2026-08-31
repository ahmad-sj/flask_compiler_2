from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/list')
def one():
    x = 1
    return render_template('i.jinja')

@app.route('/list')
def two():
    y = 2
    return render_template('i.jinja')
