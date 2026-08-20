# Legal Python: helper() is defined after the function that calls it.
# Module-level names are all bound before any of them runs.
from flask import Flask, render_template

app = Flask(__name__)

@app.route('/')
def index():
    return render_template('i.jinja', total=compute_total(), label=make_label())

def compute_total():
    return sum_prices(catalogue)

def make_label():
    return "Catalogue"

def sum_prices(rows):
    total = 0
    for row in rows:
        total = total + row["price"]
    return total

catalogue = [{"id": 1, "price": 10}, {"id": 2, "price": 20}]
