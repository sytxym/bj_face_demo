const http = require('http');
const https = require('https');
const url = require('url');


// ����
const PORT = 8080; // ���ش����������˿�
const TARGET_HOST = '172.17.192.15'; // Ŀ������� IP �����������޸�Ϊ�����ʵ��˵�ַ��
const TARGET_PORT = 8080; // Ŀ��������˿ڣ����޸�Ϊ�����ʵ��˶˿ڣ�
const TARGET_PROTOCOL = 'http'; // Ŀ�������Э�飬http �� https


// ��������������
const proxyServer = http.createServer((req, res) => {
    // 1. ��ȡ����� URL ·���Ͳ�ѯ����
    const parsedUrl = url.parse(req.url);
    const path = parsedUrl.pathname;
    const query = parsedUrl.query;


    // 2. ����Ŀ�� URL
    const targetUrl = `${TARGET_PROTOCOL}://${TARGET_HOST}:${TARGET_PORT}${path}${query ? '?' + query : ''}`;


    // 3. ��������ͷ��ת��ԭʼ����ͷ�����Ƴ� Host ͷ�������ͻ��
    const headers = { ...req.headers };
    delete headers.host;
    headers.host = `${TARGET_HOST}:${TARGET_PORT}`;


    // 4. ����Э��ѡ�� http �� https ģ��
    const client = TARGET_PROTOCOL === 'https' ? https : http;


    // 5. ��Ŀ���������������
    const proxyReq = client.request({
        hostname: TARGET_HOST,
        port: TARGET_PORT,
        path: `${path}${query ? '?' + query : ''}`,
        method: req.method,
        headers: headers
    }, (proxyRes) => {
        // 6. ������Ӧͷ��ת��Ŀ�����������Ӧͷ��
        res.writeHead(proxyRes.statusCode, proxyRes.headers);


        // 7. ת����Ӧ����
        proxyRes.pipe(res);
    });


    // 8. ��������
    proxyReq.on('error', (err) => {
        console.error('�����������:', err);
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            code: 500,
            message: '��������ʧ��',
            error: err.message
        }));
    });


    // 9. ת�������壨����� POST/PUT �ȴ� Body ������
    req.pipe(proxyReq);
});


// 10. ����������
proxyServer.listen(PORT, () => {
    console.log(`? ������������������`);
    console.log(`?? ���ط���: http://localhost:${PORT}`);
    console.log(`?? Ŀ���ַ: ${TARGET_PROTOCOL}://${TARGET_HOST}:${TARGET_PORT}`);
    console.log(`??  �� Ctrl + C ֹͣ������`);
});