import os, re, logging, json
from glob import glob
from datetime import datetime

#=== timing report ================
def reportTime(note, total, start_time):
    dt = datetime.now() - start_time
    logging.info("%s Records: %d Time: %s; Rate: %.2f"
        % (note, total, str(dt), total / (dt.seconds + .0001)))

#=== chromosome detection ================
def detectFileChrom(parameter, filename):
    chrom_patt = re.compile(r"(\b|\W)%s(\w+)(\b|\W)" % parameter, re.I)
    chrom_list = {str(idx) for idx in range(1, 23)} | {"M", "X", "Y"}
    qq = chrom_patt.search(os.path.basename(filename))
    assert qq is not None and qq.group(2).upper() in chrom_list, (
        "Failed to detect chrom in filename: " + filename)
    return qq.group(2).upper()

#=== file list extension ================
def extendFileList(files):
    if isinstance(files, str):
        files = [files]
    result = []
    for fname in files:
        if '*' in fname:
            result += list(glob(fname))
        else:
            result.append(fname)
    return sorted(result)

#==== Joined readers =====================
class JoinedReader:
    def __init__(self, name, readers, max_count = -1):
        self.mName = name
        self.mReaders = readers
        self.mBuffers = [reader.getNext() for reader in self.mReaders]
        self.mDone = False
        self.mMaxCount = max_count
        self.mTotal, self.mCount = 0, 0
        self.mStartTime = datetime.now()

    def close(self):
        for reader in self.mReaders:
            reader.close()
        reportTime("Done %s" % self.mName, self.mTotal, self.mStartTime)

    def isDone(self):
        return self.mDone

    def nextOne(self):
        if self.mDone:
            return None
        min_key = None
        for idx, reader in enumerate(self.mReaders):
            buf = self.mBuffers[idx]
            if buf is None:
                self.mBuffers[idx] = reader.getNext()
                buf = self.mBuffers[idx]
            if buf is not None:
                if min_key is None or min_key > buf[0]:
                    min_key = buf[0]
        if min_key is None:
            self.mDone = True
            return None
        res_seq = []
        for idx in range(len(self.mReaders)):
            buf = self.mBuffers[idx]
            if buf is not None and min_key == buf[0]:
                res_seq += buf[1]
                self.mBuffers[idx] = None
        self.mTotal += len(res_seq)
        self.mCount += 1
        if self.mMaxCount > 0 and self.mCount >= self.mMaxCount:
            logging.info("%s: stopped by count limit %d"
                % (self.mName, self.mMaxCount))
            self.mDone = True
        if self.mCount % 100000 == 0:
            reportTime(self.mName + (" at %s:%s:" % min_key),
                self.mTotal, self.mStartTime)
        return [min_key, res_seq]

#=====================================
def dumpReader(reader):
    for key, record in reader.read():
        print(json.dumps({"key": list(key)},
            ensure_ascii = False, sort_keys = True))
        print(json.dumps(record,
            ensure_ascii = False, sort_keys = True))
