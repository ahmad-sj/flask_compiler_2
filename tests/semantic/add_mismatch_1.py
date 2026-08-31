from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/')
def index():
    x = 'a' + 1
    return render_template('i.jinja')
