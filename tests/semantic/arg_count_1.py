from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

def two(a, b):
    return a

@app.route('/')
def index():
    x = two(1)
    return render_template('i.jinja')
