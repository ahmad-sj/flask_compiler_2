from os import path
app = Flask(__name__)

@app.route('/b')
def b():
    y = 2
    return render_template('i.jinja')
