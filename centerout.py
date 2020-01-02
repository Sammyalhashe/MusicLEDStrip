test = [i for i in range(120)]
n = len(test)
UPDATE_LEDS = 6
res = [0 for _ in range(n)]

times = 1
print(test)
for j in range(times):
    for i in range(0, n // 2 - 1 - UPDATE_LEDS // 2):
        # print("swapping {0} with {1}".format(test[i + UPDATE_LEDS // 2],
        #                                      test[i]))
        test[i] = test[i + UPDATE_LEDS // 2]
    for i in range(n - UPDATE_LEDS // 2 - 1, n // 2, -1):
        test[i + UPDATE_LEDS // 2] = test[i]
    # for i in range(n // 2 - 1, UPDATE_LEDS // 2 - 1, -1):
    #     # center-out left
    #     print("swapping {0} with {1}".format(test[i - UPDATE_LEDS // 2],
    #                                          test[i]))
    #     test[i - UPDATE_LEDS // 2] = test[i]
    #     # center-out  right
    #     print("swapping {0} with {1}".format(
    #         test[n - i + UPDATE_LEDS // 2 - 1], test[n - i]))
    #     test[n - i + UPDATE_LEDS // 2 - 1] = test[n - i]
    print(test)
