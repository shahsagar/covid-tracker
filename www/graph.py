import sqlite3
import sys
from collections import deque, defaultdict
from math import radians, cos, sin, asin, sqrt
from typing import Iterable

import pandas as pd

DB_LOC = 'yonsei-dataset/LifeMap_GS%d.db'
DAYS_7 = pd.Timedelta(days=7)
RADIUS = 6371  # radius of Earth in km
NUM_NODES = 12


def haversine_distance(lon1, lat1, lon2, lat2) -> float:
    lon1, lat1, lon2, lat2 = [radians(l) for l in [lon1, lat1, lon2, lat2]]

    dlat, dlon = lat2 - lat1, lon2 - lon1
    a = sin(dlat / 2) ** 2 + cos(lat1) * cos(lat2) * sin(dlon / 2) ** 2
    c = 2 * asin(sqrt(a))
    return c * RADIUS


def fetch_all(db: sqlite3.Connection, sql: str, parameters=None) -> (Iterable[str], Iterable[Iterable]):
    if parameters is None:
        parameters = []
    cursor = db.cursor()
    try:
        cursor.execute(sql, parameters)
        cols = [c[0] for c in cursor.description]
        return cols, cursor.fetchall()
    finally:
        cursor.close()


if __name__ == '__main__':
    try:
        leftid = int(sys.argv[1])
        enddatetime = pd.to_datetime(sys.argv[2], utc=True)
    except:
        print('[Usage] python graph.py nodeid datetime')
        exit()

    nodeid_to_data = {}
    for i in range(1, NUM_NODES + 1):
        db = sqlite3.connect(DB_LOC % i)
        sql = 'SELECT * from locationTable'
        cols, data = fetch_all(db, sql)
        df = pd.DataFrame(data, columns=cols)
        df['time'] = pd.to_datetime(df['_time_location'], utc=True)
        nodeid_to_data[i] = df

    graph = defaultdict(list)
    queue = deque([[leftid, enddatetime]])
    done = {leftid}
    while queue:
        leftid, enddatetime = queue.popleft()
        left = nodeid_to_data[leftid]
        startdatetime = enddatetime - DAYS_7
        leftlocations = left[(left['time'] > startdatetime) & (left['time'] <= enddatetime)]

        for rightid in nodeid_to_data:
            if rightid in done:
                continue
            right = nodeid_to_data[rightid]
            rightlocations = right[(right['time'] > startdatetime) & (right['time'] <= enddatetime)]
            meetings = leftlocations.merge(
                rightlocations,
                left_on=[leftlocations.time.dt.day, leftlocations.time.dt.hour, leftlocations.time.dt.minute],
                right_on=[rightlocations.time.dt.day, rightlocations.time.dt.hour, rightlocations.time.dt.minute]
            )
            # print(f'{leftid} and {rightid} had {meetings.shape[0]} meetings')
            for (_, row) in meetings.iterrows():
                haversine = haversine_distance(row['_longitude_x'], row['_latitude_x'],
                                               row['_longitude_y'], row['_latitude_y'])
                haversine /= 10 ** 6
                if haversine < 5:
                    done.add(rightid)
                    queue.append([rightid, row['time_y']])
                    graph[leftid].append(rightid)
                    break

    adjacency = [[0] * NUM_NODES for _ in range(NUM_NODES)]
    # print(graph)
    for u in graph:
        for v in graph[u]:
            adjacency[u - 1][v - 1] = 1
            adjacency[v - 1][u - 1] = 1
    print('\n'.join(map(str, adjacency)))
