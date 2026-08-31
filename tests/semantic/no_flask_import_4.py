products = [1, 2]
app = Flask(__name__)

@app.route('/c')
def c():
    z = 3
    return render_template('i.jinja')
