from flask import Flask, render_template
app = Flask(__name__)
products = [{"id": 1, "name": "A"}, {"id": 2, "name": "B"}]

@app.route('/')
def index():
    return render_template('sub/i.jinja', products=products)
