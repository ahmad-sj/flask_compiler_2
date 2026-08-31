from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/d')
def d():
    z = 7
    for q in z:
        p = q
    return render_template('i.jinja')
