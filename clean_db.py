import subprocess

def run_cmd(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error: {result.stderr.strip()}")
    return result.stdout.strip()

print("=== 데이터베이스 스키마 유지 및 데이터 완전 청소 시작 ===")

# 테이블 목록 가져오기
print("1. 테이블 목록 조회 중...")
get_tables_cmd = 'docker exec -i mysql mysql -uroot -proot1234 harudiary_db -B -N -e "SHOW TABLES;"'
tables_output = run_cmd(get_tables_cmd)

tables = [t for t in tables_output.split('\n') if t.strip()]

if not tables:
    print("삭제할 테이블이 존재하지 않습니다. (DB가 비어있음)")
else:
    print(f"[발견된 테이블 총 {len(tables)}개]: {', '.join(tables)}")
    
    # 외래키 제약조건 무시하고 모든 테이블 Truncate 수행
    print("\n2. 내부 데이터 삭제(TRUNCATE) 진행 중...")
    queries = ["SET FOREIGN_KEY_CHECKS = 0;"]
    for table in tables:
        queries.append(f"TRUNCATE TABLE {table};")
    queries.append("SET FOREIGN_KEY_CHECKS = 1;")
    
    full_query = "".join(queries)
    
    clean_cmd = f'docker exec -i mysql mysql -uroot -proot1234 harudiary_db -e "{full_query}"'
    run_cmd(clean_cmd)
    
    print("\n=== 데이터베이스 청소 완료! (스키마는 안전하게 유지되었습니다) ===")
