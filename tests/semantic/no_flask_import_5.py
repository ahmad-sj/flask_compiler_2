from json import dumps
app = Flask(__name__)

@app.route('/d')
def d():
    w = 4
    return render_template('i.jinja')
