from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/d')
def d():
    t = True
    t = False
    return render_template('i.jinja')
