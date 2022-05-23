# import os
import sys

# 1 - STANZA
# 2 - SPACY

arguments = sys.argv[2]

if arguments == '1':
    # 1 STANZA
    from conllu import parse_incr
    import stanza
    from stanza.utils.conll import CoNLL

    nlp = stanza.Pipeline('ro')
    sentence = sys.argv[1]
    while sentence.find('|') != -1:
        sentence = sentence.replace('|', '"')

    result = nlp(sentence)

    d = CoNLL.doc2conll_text(result)
    d = '# text = ' + sentence + '\n' + d[:-1]
    i = 0
    conll = ''

    for line in d.split('\n'):
        if (line.split('\t')[0] != '1'):
            conll = conll + line + '\n'
        else :
            conll = conll + '# sent_id = ' + str(i) + '\n' + line + '\n'
            i += 1

    f = open('ConlluSentence.conllu', 'w', encoding='utf-8')
    f.write(conll)
    f.close()

elif arguments == '2':
    # 2 SPACY
    import spacy
    from spacy_conll import init_parser
    from spacy_conll.parser import ConllParser

    nlp = ConllParser(init_parser("ro_core_news_sm", "spacy"))
    sentence = sys.argv[1]
    while sentence.find('|') != -1:
        sentence = sentence.replace('|', '"')

    doc = nlp.parse_text_as_conll(sentence)
    d = '# text = ' + sentence + '\n' + doc[:-1]
    i = 0
    conll = ''

    for line in d.split('\n'):
        if (line.split('\t')[0] != '1'):
            conll = conll + line + '\n'
        else:
            conll = conll + '# sent_id = ' + str(i) + '\n' + line + '\n'
            i += 1
    conll = conll + '\n'

    f = open('ConlluSentence.conllu', 'w', encoding='utf-8')
    f.write(conll)
    f.close()
