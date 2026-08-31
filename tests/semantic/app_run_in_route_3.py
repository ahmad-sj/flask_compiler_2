from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/b')
def b():
    x = 1
    app.run()
    return render_template('i.jinja')
