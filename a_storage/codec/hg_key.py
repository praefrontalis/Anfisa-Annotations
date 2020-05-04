from bisect import bisect_left

#===============================================
HG19_Tab = [
    # chrom         start           bound-length    real-length
    ["chrM",        1048576,        2097152,        16571],
    ["chr1",        3145728,        250609664,      249250621],
    ["chr2",        253755392,      244318208,      243199373],
    ["chr3",        498073600,      199229440,      198022430],
    ["chr4",        697303040,      192937984,      191154276],
    ["chr5",        890241024,      182452224,      180915260],
    ["chr6",        1072693248,     173015040,      171115067],
    ["chr7",        1245708288,     160432128,      159138663],
    ["chr8",        1406140416,     147849216,      146364022],
    ["chr9",        1553989632,     142606336,      141213431],
    ["chr10",       1696595968,     137363456,      135534747],
    ["chr11",       1833959424,     136314880,      135006516],
    ["chr12",       1970274304,     135266304,      133851895],
    ["chr13",       2105540608,     116391936,      115169878],
    ["chr14",       2221932544,     109051904,      107349540],
    ["chr15",       2330984448,     103809024,      102531392],
    ["chr16",       2434793472,     92274688,       90354753],
    ["chr17",       2527068160,     82837504,       81195210],
    ["chr18",       2609905664,     79691776,       78077248],
    ["chr19",       2689597440,     60817408,       59128983],
    ["chr20",       2750414848,     65011712,       63025520],
    ["chr21",       2815426560,     49283072,       48129895],
    ["chr22",       2864709632,     52428800,       51304566],
    ["chrX",        2917138432,     157286400,      155270560],
    ["chrY",        3074424832,     60817408,       59373566]
    # upper bound: 3135242240 == 0xbae00000
]

#===============================================
HG38_Tab = [
    # chrom         start           bound-length    real-length
    ["chrM",        1048576,        2097152,        16569],
    ["chr1",        3145728,        250609664,      248956422],
    ["chr2",        253755392,      243269632,      242193529],
    ["chr3",        497025024,      200278016,      198295559],
    ["chr4",        697303040,      191889408,      190214555],
    ["chr5",        889192448,      183500800,      181538259],
    ["chr6",        1072693248,     171966464,      170805979],
    ["chr7",        1244659712,     160432128,      159345973],
    ["chr8",        1405091840,     146800640,      145138636],
    ["chr9",        1551892480,     139460608,      138394717],
    ["chr10",       1691353088,     135266304,      133797422],
    ["chr11",       1826619392,     136314880,      135086622],
    ["chr12",       1962934272,     135266304,      133275309],
    ["chr13",       2098200576,     116391936,      114364328],
    ["chr14",       2214592512,     109051904,      107043718],
    ["chr15",       2323644416,     103809024,      101991189],
    ["chr16",       2427453440,     92274688,       90338345],
    ["chr17",       2519728128,     84934656,       83257441],
    ["chr18",       2604662784,     81788928,       80373285],
    ["chr19",       2686451712,     59768832,       58617616],
    ["chr20",       2746220544,     66060288,       64444167],
    ["chr21",       2812280832,     48234496,       46709983],
    ["chr22",       2860515328,     52428800,       50818468],
    ["chrX",        2912944128,     157286400,      156040895],
    ["chrY",        3070230528,     58720256,       57227415]
    #  Upper bound:  3128950784 == 0xba800000
]

#===============================================
class FastaConvertor:
    def __init__(self, fasta_tab, tp):
        self.mType = tp
        self.mTab = fasta_tab
        self.mStarts = [it[1] for it in self.mTab]
        self.mDict = {it[0]: it[1] for it in self.mTab}

    def getType(self):
        return self.mType

    def encode(self, chrom, pos = None):
        if isinstance(chrom, tuple):
            chrom, pos = chrom
        long_pos = self.mDict[chrom] + pos
        return (long_pos).to_bytes(4, byteorder='big')

    def decode(self, key):
        long_pos = int.from_bytes(key, 'big')
        idx = bisect_left(long_pos)
        return (self.mTab[idx][0], long_pos - self.mStarts[idx])


#===============================================
Conv_HG19 = FastaConvertor(HG19_Tab, "hg19")
Conv_HG38 = FastaConvertor(HG38_Tab, "hg38")
