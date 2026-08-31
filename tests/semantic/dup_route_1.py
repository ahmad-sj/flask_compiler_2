from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/')
def a():
    x = 1
    return render_template('i.jinja')

@app.route('/')
def b():
    y = 2
    return render_template('i.jinja')
