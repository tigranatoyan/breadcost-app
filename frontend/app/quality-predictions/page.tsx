'use client';
import { useState, useCallback } from 'react';
import { apiFetch, TENANT_ID } from '@/lib/api';
import { Spinner, Alert, Success } from '@/components/ui';
import { SectionTitle, Button, Card, StatCard, Table } from '@/components/design-system';
import { useT } from '@/lib/i18n';
import { ShieldAlert, TrendingDown, CheckCircle, RefreshCw } from 'lucide-react';

interface QualityPrediction {
  predictionId: string;
  productId: string;
  productName: string;
  riskLevel: string;
  predictedYieldPct: number;
  historicalAvgYieldPct: number;
  riskFactors: string;
  recommendation: string;
  confidence: number;
  status: string;
}

export default function QualityPredictionsPage() {
  const t = useT();
  const [predictions, setPredictions] = useState<QualityPrediction[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [filterRisk, setFilterRisk] = useState<string>('ALL');

  const generate = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await apiFetch('/v3/ai/suggestions/quality/generate', {
        method: 'POST',
        body: JSON.stringify({ tenantId: TENANT_ID }),
      }) as QualityPrediction[];
      setPredictions(data);
      setSuccess(`Generated ${data.length} quality predictions`);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const endpoint = filterRisk === 'HIGH'
        ? `/v3/ai/suggestions/quality/high-risk?tenantId=${TENANT_ID}`
        : `/v3/ai/suggestions/quality?tenantId=${TENANT_ID}`;
      setPredictions(await apiFetch(endpoint) as QualityPrediction[]);
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, [filterRisk]);

  const dismiss = useCallback(async (id: string) => {
    try {
      await apiFetch(`/v3/ai/suggestions/quality/${id}/dismiss`, { method: 'POST' });
      setPredictions((prev) => prev.filter((p) => p.predictionId !== id));
    } catch (e) {
      setError(String(e));
    }
  }, []);

  const filtered = filterRisk === 'ALL' ? predictions : predictions.filter((p) => p.riskLevel === filterRisk);
  const highCount = predictions.filter((p) => p.riskLevel === 'HIGH').length;
  const mediumCount = predictions.filter((p) => p.riskLevel === 'MEDIUM').length;
  const lowCount = predictions.filter((p) => p.riskLevel === 'LOW').length;

  return (
    <div className="space-y-6">
      <SectionTitle
        eyebrow={t('analytics.eyebrow')}
        title="Quality Predictions"
        subtitle="AI-powered quality risk assessment based on production history"
        action={
          <div className="flex gap-2">
            <Button variant="secondary" onClick={load}><RefreshCw className="w-4 h-4 mr-1" /> Load</Button>
            <Button onClick={generate}>Generate Predictions</Button>
          </div>
        }
      />

      {error && <Alert msg={error} onClose={() => setError('')} />}
      {success && <Success msg={success} onClose={() => setSuccess('')} />}

      {/* Summary cards */}
      <div className="grid grid-cols-3 gap-4">
        <StatCard icon={ShieldAlert} label="High Risk" value={String(highCount)} hint="Immediate attention" />
        <StatCard icon={TrendingDown} label="Medium Risk" value={String(mediumCount)} hint="Monitor closely" />
        <StatCard icon={CheckCircle} label="Low Risk" value={String(lowCount)} hint="Within normal range" />
      </div>

      {/* Filter */}
      <div className="flex gap-2">
        {['ALL', 'HIGH', 'MEDIUM', 'LOW'].map((level) => (
          <button
            key={level}
            onClick={() => setFilterRisk(level)}
            className={`px-3 py-1 text-sm rounded-full font-medium transition ${
              filterRisk === level ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {level}
          </button>
        ))}
      </div>

      {loading ? <Spinner /> : (
        <Card title={`Predictions (${filtered.length})`}>
          <Table
            cols={['Product', 'Risk', 'Predicted Yield', 'Hist. Avg', 'Confidence', 'Risk Factors', '']}
            rows={filtered.map((p) => [
              p.productName,
              p.riskLevel,
              `${p.predictedYieldPct.toFixed(1)}%`,
              `${p.historicalAvgYieldPct.toFixed(1)}%`,
              `${(p.confidence * 100).toFixed(0)}%`,
              p.riskFactors,
              p.status === 'ACTIVE'
                ? '<button>Dismiss</button>'
                : p.status,
            ])}
            empty="No predictions. Click Generate to analyze production quality."
          />
          {/* Detailed cards for high risk items */}
          {filtered.filter((p) => p.riskLevel === 'HIGH').map((p) => (
            <div key={p.predictionId} className="mt-4 p-4 border border-red-200 bg-red-50 rounded-lg">
              <div className="flex justify-between items-start">
                <div>
                  <h4 className="font-semibold text-red-800">{p.productName}</h4>
                  <p className="text-sm text-red-600 mt-1">{p.riskFactors}</p>
                  <p className="text-sm mt-2 text-gray-700"><strong>Recommendation:</strong> {p.recommendation}</p>
                </div>
                <button
                  onClick={() => dismiss(p.predictionId)}
                  className="text-sm text-red-600 hover:text-red-800"
                >
                  Dismiss
                </button>
              </div>
            </div>
          ))}
        </Card>
      )}
    </div>
  );
}
