from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/a')
def alpha():
    x = alpha()
    return x
