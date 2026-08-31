from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/b')
def b():
    items = [1]
    items = [2]
    return render_template('i.jinja')
