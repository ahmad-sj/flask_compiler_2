from flask import Flask, render_template

app = Flask(__name__)

@app.route('/')
def home():
    return render_template('page.jinja')

@app.route('/about')
def about():
    return render_template('missing_file.jinja')
