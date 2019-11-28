from math import sin
from pylab import draw, pause, plot, figure, clf, show, legend
import numpy as np

FACTOR = 999


def updateAverage(average, newVal):
    return (average + newVal) / 2


def runningAvg(average, newVal, count, factor):
    return average + (newVal - average) / min(count, factor)


def moreComplex(average, newVal, count):
    return average * ((count - 1) / count) + newVal / count


# store the values to be plotted
xs = np.empty(1)
ys = np.empty([4, 1])
time = 0
average = 0
# print(xs, ys)
# print(xs.shape, ys.shape)

clf()
figure()
while True:

    newy = sin(time)
    average = updateAverage(average, newy)
    runAvg = runningAvg(average, newy, time + 1, FACTOR)
    complexAvg = moreComplex(average, newy, time + 1)

    if time == 0:
        xs[0] = time
        ys[0][0] = newy
        ys[1][0] = average
        ys[2][0] = runAvg
        ys[3, 0] = complexAvg
    else:
        xs = np.append(xs, time)
        newy = [[newy], [average], [runAvg], [complexAvg]]
        ys = np.append(ys, newy, axis=1)
    time += 1
    for i in range(len(ys)):
        plot(xs, ys[i])
    legend(["orig" if i == 0 else "avg {}".format(i) for i in range(len(ys))])
    draw()
    pause(0.005)
    if time == 50:
        break
show()
