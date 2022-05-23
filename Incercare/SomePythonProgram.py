# import sys
# # from conllu import parse_incr
# # import stanza
# # from stanza.utils.conll import CoNLL
#
# #stanza.download('ro')
# # nlp = stanza.Pipeline('ro')
#
# # propozitia = input("\nText: ")
#
# propozitia = sys.argv[1]
#
# # result=nlp(propozitia)
# #
# # #scrie in fisierul output expresia conllu
# # CoNLL.write_doc2conll(result, "output.conllu")
# #
# #
# # data_file = open("output.conllu", "r", encoding="utf-8")
# #
# # print("Done!")
#
# print(propozitia + " DA DA DA")






# import sys
#
# # take the first input argument
#
# mystring = sys.argv[1]
#
# # reverses the string, but you can do anything you like here..
# #
#
# # print(mystring[::-1])
# print("wwwwwwww")







import sys
import os
from conllu import parse_incr
import stanza
from stanza.utils.conll import CoNLL

nlp = stanza.Pipeline('ro')

sentence = sys.argv[1]
# sentence = ""
# sentence = ' '.join(sentenceList)
# print(sentence)

# sentence = "Noi și ele am fost la mare."
# result = nlp(sentence)

result = nlp(sentence)

CoNLL.write_doc2conll(result, "output.conllu")

print("Done !!!")





# import sys
# import os
#
# workingdirectory = os.getcwd()
# print('Nr of arguments : ', len(sys.argv))
# print('Argument list : ', str(sys.argv))
#
# def addition():
#     print(sys.argv[1])
#     n1 = sys.argv[1]
#     n2 = sys.argv[2]
#     nrs = [n1, n2]
#     print(nrs)
#     sum = int(n1) + int(n2)
#     print('Sum = {0}' .format(sum))
#
# addition()

















