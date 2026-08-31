from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/b')
def b():
    flag = True
    for f in flag:
        g = f
    return render_template('i.jinja')
