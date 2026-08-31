from flask import Flask, render_template, redirect, url_for, request
app = Flask(__name__)

@app.route('/b')
def beta():
    return beta() + 1
